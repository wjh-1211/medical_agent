#!/usr/bin/env python3
import json
import sys

import torch
from transformers import AutoModelForCausalLM, AutoTokenizer


def main() -> int:
    request = json.load(sys.stdin)
    model_path = request["modelPath"]
    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        model_path,
        torch_dtype="auto",
        device_map="auto",
        trust_remote_code=True,
    )

    if tokenizer.pad_token_id is None:
        tokenizer.pad_token = tokenizer.eos_token

    prompt = request["prompt"]
    inputs = tokenizer(prompt, return_tensors="pt")
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
    content = tokenizer.decode(generated_tokens, skip_special_tokens=True).strip()

    print(json.dumps({
        "content": content,
        "modelName": request.get("modelName", ""),
        "provider": "python_transformers",
    }, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:  # pragma: no cover
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
