#!/bin/bash

# Target directory in your Quarkus project
TARGET_DIR="/Users/meisele/Projects/the-main-thread/flux2-quarkus/src/main/resources/models"
# Or use relative path if running from project root:
# TARGET_DIR="./src/main/resources/models"

REPO_URL="https://huggingface.co/black-forest-labs/FLUX.2-klein-4B/resolve/main"

echo "Downloading FLUX.2 Klein models to $TARGET_DIR..."

# Create directories
mkdir -p "$TARGET_DIR/vae"
mkdir -p "$TARGET_DIR/transformer"
mkdir -p "$TARGET_DIR/text_encoder"
mkdir -p "$TARGET_DIR/tokenizer"
mkdir -p "$TARGET_DIR/scheduler"

# Helper function
download_file() {
    local path=$1
    local url="$REPO_URL/$path"
    local output="$TARGET_DIR/$path"
    
    if [ -f "$output" ]; then
        echo "  [SKIP] $path (already exists)"
    else
        echo "  [DOWN] $path"
        # -L follows redirects, -f fails on 404/server errors
        curl -L -f -o "$output" "$url"
        
        if [ $? -ne 0 ]; then
             echo "  [ERROR] Failed to download $path"
             rm -f "$output"
             exit 1
        fi
    fi
}

echo "1. Downloading VAE..."
download_file "vae/diffusion_pytorch_model.safetensors"
download_file "vae/config.json"

echo "2. Downloading Transformer..."
download_file "transformer/diffusion_pytorch_model.safetensors"
download_file "transformer/config.json"

echo "3. Downloading Text Encoder (Qwen3-4B)..."
# Confirmed: This model uses sharded weights
download_file "text_encoder/model-00001-of-00002.safetensors"
download_file "text_encoder/model-00002-of-00002.safetensors"
download_file "text_encoder/config.json"

echo "4. Downloading Tokenizer..."
download_file "tokenizer/tokenizer.json"
download_file "tokenizer/tokenizer_config.json"
download_file "tokenizer/vocab.json"
download_file "tokenizer/merges.txt"
download_file "tokenizer/special_tokens_map.json"

echo "5. Downloading Configs..."
download_file "model_index.json"

echo ""
echo "Success! Models ready in $TARGET_DIR"