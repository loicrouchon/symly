#!/usr/bin/env make
HOST_ROOT_DIR=$(shell pwd)/../../..
ROOT_DIR=/workspace
PACKAGING_DIR=$(ROOT_DIR)/tools/packaging/debian

.PHONY: build
build:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/debian-build-env:latest \
		ant -v -f "$(PACKAGING_DIR)/build.xml"

.PHONY: build-shell
build-shell:
	podman run -ti \
		-v "$(HOST_ROOT_DIR):$(ROOT_DIR)" \
		symly/debian-build-env:latest

.PHONY: build-env
build-env:
	podman build -t symly/debian-build-env:latest .
