# {{jreleaserCreationStamp}}
Name:      {{specPackageName}}
Version:   {{projectVersion}}
Release:   {{specRelease}}
Summary:   {{projectDescription}}

License:   {{projectLicense}}
URL:       {{projectWebsite}}
Source0:   {{distributionUrl}}

BuildArch: noarch
BuildRequires: java-17-openjdk-devel, ant, picocli
Requires: java, picocli
{{#specRequires}}
#Requires:  {{.}}
{{/specRequires}}

%description
{{projectLongDescription}}

%prep

%setup -q -n {{distributionArtifactFileName}}
%build
ant -f src/packaging/fedora/build.xml

%install
%define distdir build/ant/fedora/distributions
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
