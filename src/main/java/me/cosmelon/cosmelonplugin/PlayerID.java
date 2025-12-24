package me.cosmelon.cosmelonplugin;

import java.util.UUID;

public class PlayerID {

    private String name;
    private String id; // raw ID from mojang response

    public String getName() {
        return name;
    }

    public UUID getId() {
        return UUID.fromString(
                id.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                )
        );
    }
}
