#!/usr/bin/env make
ROOT_DIR=$(shell pwd)/../../..
PACKAGING_DIR=tools/packaging/homebrew

.PHONY: build
build:
	cd $(ROOT_DIR) && \
		mvn --batch-mode --settings "$(PACKAGING_DIR)/settings.xml" \
			clean verify
