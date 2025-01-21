# print the available options
help:
    just -l

# Installs Words! local as if was built for Flathub
flathub-install:
    cd ../page.codeberg.petsoi.words && flatpak run org.flatpak.Builder --force-clean --sandbox --user --install --install-deps-from=flathub --ccache --mirror-screenshots-url=https://dl.flathub.org/media/ --repo=repo builddir page.codeberg.petsoi.words.json
    
# Executes all linters for Flathub
flathub-lint:
    flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest ../page.codeberg.petsoi.words/page.codeberg.petsoi.words.json

# Update the flatpak dependencies 
flathub-update-cargo-dependencies:
    python3 helper/flatpak-cargo-generator.py Cargo.lock -o ../page.codeberg.petsoi.words/cargo-sources.json

# Entering shell for python
enter-shell:
    poetry shell