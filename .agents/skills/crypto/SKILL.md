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

class Foobar
{
	PGPSecretKeyRingCollection secretKeys = getSecretKey();
	PGPPublicKeyRingCollection publicKeys = getPublicKey();

	// Encrypt
	var encryptorBuilder = new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_128)
			.setWithIntegrityPacket(true) // Required to guarantee integrity
			.setSecureRandom(SecureRandomUtils.getGenerator());
	var encryptedDataGenerator = new PGPEncryptedDataGenerator(encryptorBuilder);
	encryptedDataGenerator.addMethod(new

	JcePublicKeyKeyEncryptionMethodGenerator(publicKeys));

	// Decrypt
	PGPPrivateKey privateKey = secretKeys.getSecretKey(keyId)
			.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
					.setProvider("BC")
					.build(passphrase.toCharArray()));
}
```

See `app/src/main/java/io/xeres/app/crypto/pgp/PGP.java` for real-world usage.

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

class Foobar
{
	static
	{
		Security.addProvider(new BouncyCastleProvider());
	}

	MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
	byte[] hash = digest.digest(data);
}
```

## Best Practices

1. Use the `SecureRandomUtils` class for all random operations
2. Prefer JCA/JCE, otherwise use BouncyCastle
3. Use constant-time comparisons for secrets
4. Clear sensitive data from memory when done
5. Use appropriate key sizes (RSA 2048+)

## Identifier Classes

Cryptographic identifiers implement `Identifier`:

```java
public class GxsId implements Identifier, Comparable<GxsId>
{
	public static final int LENGTH = 16;
}
```

Existing identifiers are in `common/src/main/java/io/xeres/common/id/` (e.g. `GxsId`, `Sha1Sum`, `ProfileFingerprint`).
