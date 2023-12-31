
.PHONY: gh-actions-versions
gh-actions-versions:
	@grep -R '@v' workflows | cut -d':' -f3 |  sort -u
