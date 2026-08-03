package dfs.server;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * FIX 1: whitelistt-based deserialization to prevent RCE via gadget chains.
 * Replaces raw ObjectInputStream on the server side.
 */
public class ValidatingObjectInputStream extends ObjectInputStream {

    // Java 8 compatible version of Set.of()
    private static final Set<String> ALLOWED_CLASSES = new HashSet<>(Arrays.asList(
        "dfs.server.FileSerializable",
        "dfs.server.WriteRequest",
        "dfs.server.WriteRequest$OperationType",
        "java.lang.String",
        "java.util.Date",
        "[B"   // byte array
    ));

    public ValidatingObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        if (!ALLOWED_CLASSES.contains(desc.getName())) {
            // FIX 1: Block any class not in the whitelist
            throw new InvalidClassException(
                "Unauthorized deserialization blocked: " + desc.getName());
        }
        return super.resolveClass(desc);
    }
}