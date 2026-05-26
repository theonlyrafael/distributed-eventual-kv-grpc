package ds.kvstore.server;

import ds.kvstore.model.StoreEntry;
import ds.kvstore.model.VectorClock;
import ds.kvstore.model.Version;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NodeState {
    private final String nodeId;
    private final ConcurrentHashMap<String, StoreEntry> store = new ConcurrentHashMap<>();

    public NodeState(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId(){ return nodeId; }

    public Version processPut(String key, String value) {
        // Build vector clock merged from actives, then increment own component
        VectorClock merged = new VectorClock();
        StoreEntry e = store.get(key);
        if (e != null) {
            for (Version ex : e.getActiveVersions()) {
                merged.merge(ex.getVectorClock());
            }
        }
        merged.increment(nodeId);
        Version v = new Version(value, System.nanoTime(), merged, nodeId);
        store.compute(key, (k, entry) -> {
            if (entry == null) entry = new StoreEntry();
            entry.addVersion(v);
            return entry;
        });
        return v;
    }

    public void applyRemote(String key, Version v) {
        store.compute(key, (k, entry) -> {
            if (entry == null) entry = new StoreEntry();
            entry.addVersion(v);
            return entry;
        });
    }

    public List<Version> getActive(String key) {
        StoreEntry e = store.get(key);
        return e == null ? java.util.List.of() : e.getActiveVersions();
    }
}
