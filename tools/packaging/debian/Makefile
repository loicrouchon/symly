#!/usr/bin/env make
HOST_ROOT_DIR=$(shell pwd)/../../..
ROOT_DIR=/workspace
PACKAGING_DIR=$(ROOT_DIR)/tools/packaging/debian

.PHONY: build
build:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/ubuntu-build-env:latest \
		"$(PACKAGING_DIR)/build.sh" "$(VERSION)"

.PHONY: publish
publish:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/ubuntu-build-env:latest \
		"$(PACKAGING_DIR)/publish.sh" "$(VERSION)"

.PHONY: shell
shell:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/ubuntu-build-env:latest

.PHONY: build-env
build-env:
	podman build -t symly/ubuntu-build-env:latest .
