package ds.kvstore.server;

import ds.kvstore.model.Version;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class NodeStateTest {

    @Test
    void testProcessPutIncrementsOwnClock() {
        NodeState state = new NodeState("node_A");
        Version v1 = state.processPut("k", "v1");
        assertEquals(1L, v1.getVectorClock().get("node_A"));
        assertEquals("v1", v1.getValue());

        Version v2 = state.processPut("k", "v2");
        assertEquals(2L, v2.getVectorClock().get("node_A"));
        var actives = state.getActive("k");
        assertEquals(1, actives.size());
        assertEquals("v2", actives.get(0).getValue());
    }

    @Test
    void testApplyRemoteConcurrent() {
        NodeState a = new NodeState("node_A");
        NodeState b = new NodeState("node_B");

        // A escreve local
        var vA = a.processPut("k", "A1");

        // B escreve local em paralelo (concorrente)
        var vB = b.processPut("k", "B1");

        // Replica cruzada
        a.applyRemote("k", vB);
        b.applyRemote("k", vA);

        List<Version> aActives = a.getActive("k");
        List<Version> bActives = b.getActive("k");

        assertEquals(2, aActives.size());
        assertEquals(2, bActives.size());
    }
}
