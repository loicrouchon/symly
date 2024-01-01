class {{brewFormulaName}} < Formula
  desc "{{projectDescription}}"
  homepage "{{projectWebsite}}"
  url "{{distributionUrl}}"
  sha256 "{{distributionChecksumSha256}}"
  license "{{projectLicense}}"

  {{#brewHasLivecheck}}
  livecheck do
    {{#brewLivecheck}}
    {{.}}
    {{/brewLivecheck}}
  end
  {{/brewHasLivecheck}}
  {{#brewDependencies}}
  depends_on {{.}}
  {{/brewDependencies}}

  def install
    prefix.install Dir["*"]
  end

  test do
    output = shell_output("#{bin}/{{distributionExecutable}} --version")
    assert_match "{{projectVersion}}", output
  end
end
