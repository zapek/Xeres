---
name: crypto
description: Cryptography patterns for Xeres including PGP operations, key generation, and hash functions with best practices.
---

# Cryptography Patterns for Xeres

## JCE/JCA and BouncyCastle Usage

Xeres uses JCE/JCA and BouncyCastle for cryptographic operations. Always use the registered providers.

## Common Patterns

### OpenPGP Operations

```java
import org.bouncycastle.openpgp.*;

PGPSecretKeyRingCollection secretKeys = ...
PGPPublicKeyRingCollection publicKeys = ...

// Encrypt
PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
		new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
				.setWithIntegrityPacket(true)
				.setSecureRandom(new SecureRandom())
				.useInsecureRandom() // Only for testing
);

// Decrypt
PGPPrivateKey privateKey = secretKeys.getSecretKey(keyId)
		.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
				.setProvider("BC")
				.build(passphrase.toCharArray()));
```

### Key Generation

```java
import org.bouncycastle.bcpg.*;
import org.bouncycastle.openpgp.*;

var keyRingGenerator = new PGPKeyRingGenerator(
		V3PGPSignature.POSITIVE_CERTIFICATION,
		new PGPSignatureSubpacketGenerator(),
		algorithm,
		encryptionKey,
		creationTime,
		"User ID",
		symmetricKeyEncryption,
		hashedGen,
		unhashedGen,
		new SecureRandom(),
		"BC"
);
```

### Hash Functions

```java
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.MessageDigest;

Security.addProvider(new BouncyCastleProvider());
MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
byte[] hash = digest.digest(data);
```

## Best Practices

1. Use the `SecureRandomUtils` class for all random operations
2. Prefer JCA/JCE, otherwise use BouncyCastle
3. Use constant-time comparisons for secrets
4. Clear sensitive data from memory when done
5. Use appropriate key sizes (RSA 2048+)

## Identifier Classes

Cryptographic identifiers extend `Identifier`:

```java
public class RsPkIdentifier extends Identifier
{
	public static final int LENGTH = 16;
}
```
