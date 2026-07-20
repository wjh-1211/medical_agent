#!/usr/bin/env python3
import argparse
import json
import sys

import torch
import torch.nn.functional as functional
from transformers import AutoModel, AutoTokenizer


def emit(message: dict) -> None:
    print(json.dumps(message, ensure_ascii=False), flush=True)


def last_token_pool(last_hidden_states, attention_mask):
    left_padding = attention_mask[:, -1].sum() == attention_mask.shape[0]
    if left_padding:
        return last_hidden_states[:, -1]
    sequence_lengths = attention_mask.sum(dim=1) - 1
    return last_hidden_states[torch.arange(last_hidden_states.shape[0], device=last_hidden_states.device), sequence_lengths]


def load_model(model_path: str):
    tokenizer = AutoTokenizer.from_pretrained(model_path, padding_side="left", trust_remote_code=True)
    model = AutoModel.from_pretrained(
        model_path,
        torch_dtype="auto",
        device_map="auto",
        trust_remote_code=True,
    )
    model.eval()
    return tokenizer, model


def embed(texts, mode, tokenizer, model, dimension, query_instruction):
    prepared = texts
    if mode == "query":
        prepared = [f"Instruct: {query_instruction}\nQuery:{text}" for text in texts]
    batch = tokenizer(prepared, padding=True, truncation=True, max_length=8192, return_tensors="pt")
    device = next(model.parameters()).device
    batch = {key: value.to(device) for key, value in batch.items()}
    with torch.no_grad():
        outputs = model(**batch)
    embeddings = last_token_pool(outputs.last_hidden_state, batch["attention_mask"])
    embeddings = embeddings[:, :dimension]
    embeddings = functional.normalize(embeddings, p=2, dim=1)
    return embeddings.float().cpu().tolist()


def run_server():
    init = json.loads(sys.stdin.readline())
    if init.get("type") != "init":
        raise ValueError("embedding server init message is required")
    tokenizer, model = load_model(init["modelPath"])
    dimension = int(init["dimension"])
    query_instruction = init.get("queryInstruction", "Retrieve relevant passages")
    emit({"type": "ready"})
    for line in sys.stdin:
        if not line.strip():
            continue
        request = json.loads(line)
        if request.get("type") != "embed":
            continue
        vectors = embed(request["texts"], request.get("mode", "document"), tokenizer, model, dimension, query_instruction)
        emit({"type": "response", "requestId": request.get("requestId", ""), "vectors": vectors})


if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument("--server", action="store_true")
        arguments = parser.parse_args()
        if not arguments.server:
            raise ValueError("--server is required")
        run_server()
    except Exception as exception:  # pragma: no cover
        print(str(exception), file=sys.stderr)
        raise SystemExit(1)
