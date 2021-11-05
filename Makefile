GRAALVM_VERSION=21.3.0
GRAALVM_BASE_URL=https://github.com/graalvm/graalvm-ce-builds/releases/download
GRAALVM_BASE_NAME=graalvm-ce-java17
OS=$(shell uname)

ifeq ($(OS), Darwin)
	GRAALVM_ARCH=$(GRAALVM_BASE_NAME)-darwin-amd64
	GRAALVM_LOCATION=tools/$(GRAALVM_ARCH)-$(GRAALVM_VERSION)/Contents/Home
else ifeq ($(OS), Linux)
	ARCH=$(shell uname -m)
	ifeq ($(ARCH), x86_64)
		GRAALVM_ARCH=$(GRAALVM_BASE_NAME)-linux-amd64
	else ifeq ($(ARCH), aarch64)
		GRAALVM_ARCH=$(GRAALVM_BASE_NAME)-linux-aarch64
	else
$(error Only x86_64 and aarch64 are supported linux architectures)
	endif
	GRAALVM_LOCATION=tools/$(GRAALVM_ARCH)-$(GRAALVM_VERSION)
else
$(error Only Darwin and Linux operating systems are supported)
endif

GRAALVM_NAME=$(GRAALVM_ARCH)-$(GRAALVM_VERSION)
GRAALVM_TGZ=tools/$(GRAALVM_NAME).tar.gz
GRAALVM_URL=$(GRAALVM_BASE_URL)/vm-$(GRAALVM_VERSION)/$(GRAALVM_NAME).tar.gz
JAVA_HOME=$(shell pwd)/$(GRAALVM_LOCATION)

.PHONY: clean install-requirements build

build: install-requirements

install-requirements: $(JAVA_HOME)
	@rm -f tools/graalvm
	@ln -s $(JAVA_HOME) tools/graalvm

$(JAVA_HOME): $(GRAALVM_TGZ)
	@echo "Installing Graal VM $(GRAALVM_TGZ) to tools/$(GRAALVM_NAME)"
	@rm -rf tools/$(GRAALVM_NAME)
	@tar xzfm tools/$(GRAALVM_NAME).tar.gz --directory tools/
	@mv tools/$(GRAALVM_BASE_NAME)-$(GRAALVM_VERSION) tools/$(GRAALVM_NAME)
	@ls -l $(GRAALVM_TGZ)

$(GRAALVM_TGZ):
	@echo "Downloading Graal VM $(GRAALVM_URL)"
	@mkdir -p tools
	@curl --silent -L $(GRAALVM_URL) -o tools/$(GRAALVM_NAME).tar.gz
	@tar xzf tools/$(GRAALVM_NAME).tar.gz --directory tools/
	@mv tools/$(GRAALVM_BASE_NAME)-$(GRAALVM_VERSION) tools/$(GRAALVM_NAME)

clean:
	@rm -rf tools
