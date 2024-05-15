# JavaFX best practices

## Layout

### Minimum sizes of windows

Usually 240 per 200 as minimum. Needs to be specified to the root of the container in the FXML.

Useful constants:

- `USE_COMPUTED_SIZE` = _-1_
- `USE_PREFS_SIZE` = _-Infinity_

`minWidth` and `minHeight` can use _USE_PREFS_SIZE_ which means they'll match `prefWidth` and `prefHeight`. While _USE_COMPUTED_SIZE_ might appear to be useful, in practice it rarely works, being either too greedy (masked buttons, ugly widgets) or allowing the window to hide the whole interface.

## TableView, ListView, TreeView

### PropertyValueFactory

Property value factory work and can track changes from the original data (they use observables) but are not very flexible, are prone to typos/wrong names that are not detected by the compiler and don't work with records. Instead, try to use the following:

```java
TableColumn<Person, String> lastColumn = new TableColumn<>("Last");
lastColumn.setCellValueFactory(
        p -> new SimpleStringProperty(p.getValue().last())
);
```

For other things than a string, use:

```java
TableColumn<Person, Integer> ageColumn = new TableColumn<>("Age");
ageColumn.setCellValueFactory(
        p -> new SimpleIntegerProperty(p.getValue().age()).asObject()
);
```

There's also SimpleObjectProperty<MyType> and so on...

### FXML

One can do it with FXML only like:

```java
<TableView fx:id="todoList">
    <columns>
        <TableColumn text="Name" minWidth="75.0" sortable="true">
            <cellValueFactory>
                <PropertyValueFactory property="name" />
            </cellValueFactory>
        </TableColumn>
        <TableColumn text="Date" minWidth="50.0" sortable="true">
            <cellValueFactory>
                <PropertyValueFactory property="date" />
            </cellValueFactory>
        </TableColumn>
    </columns>
</TableView>
```
