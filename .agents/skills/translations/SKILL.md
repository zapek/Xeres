---
name: translations
description: How to translate the help files and the i18n properties files
---

# Translations rules for Xeres

## Supported languages

The source language is English (en).

The target languages are:

- French (fr)
- Spanish (es)
- Russian (ru)
- Chinese (zh)

## Files to modify

### properties files

The properties files are located in the `common` module, in `src/main/resources/i18n`. The english file is `messages.properties` and the translations have the same filename with the language's code appended, for example `messages_fr.properties` for French.

The master file is the English one. All translations need to follow the same structure, including key names, their order and the comments in the file (prefixed by `#`)

### help files

The help files are Markdown files located in the `ui` module, in `src/main/resources/help`. They're organized by subdirectories with the name of the language code, for example the subdirectory `en` contains the English language.

Except for `00.Index.md` which is the main index file, all other files should have their name in the language translation. The prefix (for example `03a` must not be translated).

For example: `01.Getting Started.md` gives `01.Configuración rápida.md` for Spanish.

If there are inline images (using data: base64 URIs), they must be copied into the translation as-is.

## Changes

To quickly find what needs to be done, check when the properties translations were last modified, then check all modifications in `messages.properties` since then. Then apply all modifications, additions, removals to the translation files.

A similar way has to be done for the help files.
