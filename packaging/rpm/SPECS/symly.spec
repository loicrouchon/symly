Name:           symly
Version:        ${version}
Release:        1%{?dist}
Summary:        Manages symbolic links.

License:        ASL 2.0
URL:            https://github.com/loicrouchon/symly

BuildArch: x86_64

%description
Symly is a tool for deploying and managing symbolic links to a set of files.

%install
echo "BUILDROOT = %{buildroot}"
echo "PWD = \$(pwd)"
echo "-----------------"
mkdir -p "%{buildroot}/usr"
cp -R "usr" "%{buildroot}"

%files
%license /usr/share/doc/symly/LICENSE
%attr(0755, root, root)/usr/bin/symly
%attr(0644, root, root)/usr/man/man1/symly*
