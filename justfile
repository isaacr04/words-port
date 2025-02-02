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

# clean up dictionary
clean-words name:
    #!/bin/bash
    if [ ! -f "data/resources/word-lists/{{ name }}" ]; then
        echo "Error: File '{{ name }}' does not exist!" >&2
        exit 1
    fi
    tr a-z A-Z < data/resources/word-lists/{{name}} | sort | uniq > data/resources/word-lists/words_new.txt
    mv data/resources/word-lists/words_new.txt data/resources/word-lists/{{name}}

# Freshly build and install Flatpak on machine
install:
    flatpak-builder --user --force-clean --install build-dir page.codeberg.petsoi.words.json

# Run program inside Flatpak
run:
    flatpak-builder --run build-dir page.codeberg.petsoi.words.json words

# Removes all words which are less the 4 and more than 12 letters, no header
remove-incorrect-words name:
    awk 'length($0) >= 4 && length($0) <= 11' "data/resources/word-lists/{{ name }}" > "data/resources/word-lists/tmp.txt"
    mv data/resources/word-lists/tmp.txt "data/resources/word-lists/{{ name }}"


