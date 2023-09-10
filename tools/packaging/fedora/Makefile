#!/usr/bin/env make
HOST_ROOT_DIR=$(shell pwd)/../../..
ROOT_DIR=/workspace
PACKAGING_DIR=$(ROOT_DIR)/tools/packaging/fedora

.PHONY: build
build:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/fedora-build-env:latest \
		"$(PACKAGING_DIR)/build-from-spec.sh"

.PHONY: build-shell
build-shell:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/fedora-build-env:latest

.PHONY: build-env
build-env:
	podman build -t symly/fedora-build-env:latest .
