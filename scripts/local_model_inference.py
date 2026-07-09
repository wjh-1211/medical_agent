#!/usr/bin/env python3
import argparse
import json
import sys

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

WARMUP_PROMPT = """# Local Medical Agent Warmup
You are the local reasoning model for a memory-first medical agent framework.

Current User Message:
你好

Conversation History:
No prior history.

Memory Summary:
N/A

Tool Facts:
{}

Emergency Flag:
unknown

Metadata:
channel=cli

Available Tools:
[]

Observation History:
No observations yet.

Response Contract:
- You must respond with JSON only.
- If you need to use a tool, respond with:
  {"type":"tool_call","toolName":"echo_input","input":{"message":"warmup"}}
- If you can answer the user now, respond with:
  {"type":"final_answer","answer":"warmup"}
"""
ASSISTANT_JSON_PREFIX = "<｜Assistant｜>{"


def emit(message: dict) -> None:
    print(json.dumps(message, ensure_ascii=False), flush=True)


def load_model(model_path: str, emit_status: bool):
    if emit_status:
        emit({
            "type": "status",
            "progress": 10,
            "message": "正在加载 tokenizer"
        })
    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)

    if emit_status:
        emit({
            "type": "status",
            "progress": 35,
            "message": "正在加载模型权重"
        })
    model = AutoModelForCausalLM.from_pretrained(
        model_path,
        torch_dtype="auto",
        device_map="auto",
        trust_remote_code=True,
    )
    model.eval()

    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token

    if emit_status:
        emit({
            "type": "status",
            "progress": 85,
            "message": "正在预热推理引擎"
        })
    warmup_model(tokenizer, model)
    return tokenizer, model


def generate_content(request: dict, tokenizer, model) -> str:
    prompt = request["prompt"]
    model_input = tokenizer.apply_chat_template(
        [{"role": "user", "content": prompt}],
        tokenize=False,
        add_generation_prompt=False,
    )
    model_input = model_input + ASSISTANT_JSON_PREFIX
    inputs = tokenizer(model_input, return_tensors="pt")
    inputs = {key: value.to(model.device) for key, value in inputs.items()}

    do_sample = request.get("temperature", 0.0) > 0
    generation_kwargs = {
        "max_new_tokens": int(request.get("maxTokens", 256)),
        "do_sample": do_sample,
        "pad_token_id": tokenizer.pad_token_id,
    }
    if do_sample:
        generation_kwargs["temperature"] = float(request.get("temperature", 0.2))

    with torch.no_grad():
        outputs = model.generate(**inputs, **generation_kwargs)

    prompt_tokens = inputs["input_ids"].shape[-1]
    generated_tokens = outputs[0][prompt_tokens:]
    decoded = tokenizer.decode(generated_tokens, skip_special_tokens=True).strip()
    return normalize_generated_content("{" + decoded)


def normalize_generated_content(decoded: str) -> str:
    content = decoded.strip()
    if "</think>" in content:
        content = content.split("</think>")[-1].strip()
    if "<｜Assistant｜>" in content:
        content = content.split("<｜Assistant｜>")[-1].strip()
    return content


def warmup_model(tokenizer, model) -> None:
    warmup_request = {
        "prompt": WARMUP_PROMPT,
        "temperature": 0.0,
        "maxTokens": 16,
    }
    _ = generate_content(warmup_request, tokenizer, model)
    if torch.cuda.is_available():
        torch.cuda.synchronize()


def run_server() -> int:
    init_request = json.loads(sys.stdin.readline())
    if init_request.get("type") != "init":
        raise ValueError("server init message is required")
    model_path = init_request["modelPath"]
    tokenizer, model = load_model(model_path, emit_status=True)

    emit({
        "type": "ready",
        "progress": 100,
        "message": "模型已就绪"
    })
    for line in sys.stdin:
        if not line.strip():
            continue
        request = json.loads(line)
        emit_response(request, tokenizer, model)
    return 0


def emit_response(request: dict, tokenizer, model) -> None:
    content = generate_content(request, tokenizer, model)
    emit({
        "type": "response",
        "requestId": request.get("requestId", ""),
        "content": content,
        "modelName": request.get("modelName", ""),
        "provider": "python_transformers",
    })


def run_single() -> int:
    request = json.load(sys.stdin)
    model_path = request["modelPath"]
    tokenizer, model = load_model(model_path, emit_status=False)
    content = generate_content(request, tokenizer, model)

    print(json.dumps({
        "content": content,
        "modelName": request.get("modelName", ""),
        "provider": "python_transformers",
    }, ensure_ascii=False))
    return 0



if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument("--server", action="store_true")
        args = parser.parse_args()
        raise SystemExit(run_server() if args.server else run_single())
    except Exception as exc:  # pragma: no cover
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
