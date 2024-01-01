# https://docs.fedoraproject.org/en-US/java-packaging-howto/packaging_maven_project/

Name:      symly
Version:   ${version}
Release:   1%{?dist}
Summary:   A tool for managing symbolic links

License:   Apache-2.0
URL:       https://github.com/loicrouchon/symly
Source0:   https://github.com/loicrouchon/symly/archive/refs/tags/v${version}.tar.gz

BuildArch: noarch
BuildRequires: java-latest-openjdk-devel, ant, picocli
Requires: java-latest-openjdk-headless, picocli

%description
Symly is a tool helping to centralize sparse file trees.
It replicates and maintains a file tree structure of one
or more repository layers into a directory by creating
symbolic links.

%prep
%setup -q -n symly-${version}

%build
cat /etc/fedora-release
ls -l /usr/lib/jvm
java --version
javac --version
echo "$JAVA_HOME"
ant -f tools/packaging/fedora/build.xml -Dproject.version="${version}" -v

%install
%define distdir target/distributions/fedora
find .
mkdir -p %{buildroot}/usr/bin %{buildroot}/usr/share/java/%{name} %{buildroot}/usr/share/man/man1/
install -p -m 755 %{distdir}/usr/bin/%{name} %{buildroot}/usr/bin/%{name}
install -p -m 644 %{distdir}/usr/share/java/%{name}/%{name}.jar %{buildroot}/usr/share/java/%{name}/%{name}.jar
install -p -m 644 %{distdir}/usr/share/man/man1/%{name}.1.gz %{buildroot}/usr/share/man/man1/%{name}.1.gz
install -p -m 644 %{distdir}/usr/share/man/man1/%{name}-link.1.gz %{buildroot}/usr/share/man/man1/%{name}-link.1.gz
install -p -m 644 %{distdir}/usr/share/man/man1/%{name}-status.1.gz %{buildroot}/usr/share/man/man1/%{name}-status.1.gz
install -p -m 644 %{distdir}/usr/share/man/man1/%{name}-unlink.1.gz %{buildroot}/usr/share/man/man1/%{name}-unlink.1.gz

%files
%license LICENSE
/usr/bin/%{name}
/usr/share/java/%{name}/%{name}.jar
/usr/share/man/man1/%{name}.1.gz
/usr/share/man/man1/%{name}-link.1.gz
/usr/share/man/man1/%{name}-status.1.gz
/usr/share/man/man1/%{name}-unlink.1.gz
