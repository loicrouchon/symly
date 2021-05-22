ifndef VERSION
VERSION=dev
endif

EXECUTABLE=build/libs/symly
INSTALL=/usr/bin/symly

GRAALVM_VERSION=21.1.0
GRAALVM_BASE_URL=https://github.com/graalvm/graalvm-ce-builds/releases/download
OS=$(shell uname)
ifeq ($(OS), Darwin)
	GRAALVM_ARCH=graalvm-ce-java11-darwin-amd64
	GRAALVM_LOCATION=tools/$(GRAALVM_ARCH)-$(GRAALVM_VERSION)/Contents/Home
else ifeq ($(OS), Linux)
	ARCH=$(shell uname -m)
	ifeq ($(ARCH), x86_64)
		GRAALVM_ARCH=graalvm-ce-java11-linux-amd64
	else ifeq ($(ARCH), aarch64)
		GRAALVM_ARCH=graalvm-ce-java11-linux-aarch64
	else
$(error Only x86_64 and aarch64 are supported linux architectures)
	endif
	GRAALVM_LOCATION=tools/$(GRAALVM_ARCH)-$(GRAALVM_VERSION)
else
$(error Only Darwin and Linux operating systems are supported)
endif

GU=$(GRAALVM_LOCATION)/lib/installer/bin/gu
GRAALVM_NAME=$(GRAALVM_ARCH)-$(GRAALVM_VERSION)
GRAALVM_TGZ=tools/$(GRAALVM_NAME).tar.gz
GRAALVM_URL=$(GRAALVM_BASE_URL)/vm-$(GRAALVM_VERSION)/$(GRAALVM_NAME).tar.gz
GRAALVM_HOME=$(shell pwd)/$(GRAALVM_LOCATION)
JAVA_HOME=$(GRAALVM_HOME)
NATIVE_IMAGE=$(GRAALVM_HOME)/bin/native-image

GRADLE_ENV=GRAALVM_HOME=$(GRAALVM_HOME) JAVA_HOME=$(JAVA_HOME)
GRADLE=$(GRADLE_ENV) ./gradlew -Dgradle.user.home=.gradle --no-daemon --console=plain -Pversion=$(VERSION)

.PHONY: clean build install install-requirements

build: $(EXECUTABLE)
binary: $(INSTALL)
clean:
	@rm -rf build
install-requirements: $(NATIVE_IMAGE)

$(GRAALVM_TGZ):
	@echo "Downloading Graal VM $(GRAALVM_URL)"
	@mkdir -p tools
	@curl --silent -L $(GRAALVM_URL) -o tools/$(GRAALVM_NAME).tar.gz
	@tar xzf tools/$(GRAALVM_NAME).tar.gz --directory tools/
	@mv tools/graalvm-ce-java11-$(GRAALVM_VERSION) tools/$(GRAALVM_NAME)

$(GU): $(GRAALVM_TGZ)
	@echo "Installing Graal VM $(GRAALVM_TGZ) to tools/$(GRAALVM_NAME)"
	@rm -rf tools/$(GRAALVM_NAME)
	@tar xzfm tools/$(GRAALVM_NAME).tar.gz --directory tools/
	@mv tools/graalvm-ce-java11-$(GRAALVM_VERSION) tools/$(GRAALVM_NAME)
	@ls -l $(GRAALVM_TGZ) $(GU)

$(NATIVE_IMAGE): $(GU)
	@echo "Installing Graal VM native-image $(NATIVE_IMAGE)"
	@$(GU) install native-image

$(EXECUTABLE): $(NATIVE_IMAGE)
	@echo "Building application $(EXECUTABLE)"
	$(GRADLE) clean buildNativeImage

$(INSTALL): $(EXECUTABLE)
	@echo "Building binary"
	@ls -l build/libs/symly
	@install -D -m 0755 $(EXEC) $(INSTALL)
