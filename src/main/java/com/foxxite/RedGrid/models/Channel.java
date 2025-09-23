package com.foxxite.RedGrid.models;

import com.foxxite.RedGrid.RedGrid;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;

import java.util.UUID;

@DatabaseTable(tableName = "channels")
public class Channel {

    @DatabaseField(id = true)
    @Getter
    private String name;

    @DatabaseField(canBeNull = false)
    @Getter
    private UUID creator;

    // One-to-many relationship to Transponder
    @ForeignCollectionField(eager = false)
    @Getter
    private ForeignCollection<Transponder> transponders;

    // ORMLite requires a no-arg constructor
    public Channel() {}

    public Channel(String name, UUID creator) {
        this.name = name;
        this.creator = creator;
    }

    public void addTransponder(Transponder transponder) {
        transponders.add(transponder);
    }

    public void removeTransponder(Transponder transponder) {
        transponders.remove(transponder);
    }
}
