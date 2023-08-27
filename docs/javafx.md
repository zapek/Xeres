# JavaFX best practices

## Layout

### Minimum sizes of windows

Usually 240 per 200 as minimum. Needs to be specified to the root of the container in the FXML.

Useful constants:

- `USE_COMPUTED_SIZE` = _-1_
- `USE_PREFS_SIZE` = _-Infinity_

`minWidth` and `minHeight` can use _USE_PREFS_SIZE_ which means they'll match `prefWidth` and `prefHeight`. While _USE_COMPUTED_SIZE_ might appear to be useful, in practice it rarely works, being either too greedy (masked buttons, ugly widgets) or allowing the window to hide the whole interface.