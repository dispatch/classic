// RSA, a suite of routines for performing RSA public-key computations in
// JavaScript.
//
// Requires BigInt.js and Barrett.js.
//
// Copyright 1998-2005 David Shapiro.
//
// You may use, re-use, abuse, copy, and modify this code to your liking, but
// please keep this header.
//
// Thanks!
// 
// Dave Shapiro
// dave@ohdave.com 

function RSAKeyPair(encryptionExponent, decryptionExponent, modulus)
{
	this.e = biFromHex(encryptionExponent);
	this.d = biFromHex(decryptionExponent);
	this.m = biFromHex(modulus);
	
	// We can do two bytes per digit, so
	// chunkSize = 2 * (number of digits in modulus - 1).
	// Since biHighIndex returns the high index, not the number of digits, 1 has
	// already been subtracted.
	//this.chunkSize = 2 * biHighIndex(this.m);
	
	////////////////////////////////// TYF
    	this.digitSize = 2 * biHighIndex(this.m) + 2;
	this.chunkSize = this.digitSize - 11; // maximum, anything lower is fine
	////////////////////////////////// TYF

	this.radix = 16;
	this.barrett = new BarrettMu(this.m);
}

function twoDigit(n)
{
	return (n < 10 ? "0" : "") + String(n);
}

function encryptedString(key, s)
// Altered by Rob Saunders (rob@robsaunders.net). New routine pads the
// string after it has been converted to an array. This fixes an
// incompatibility with Flash MX's ActionScript.
// Altered by Tang Yu Feng for interoperability with Microsoft's
// RSACryptoServiceProvider implementation.
{
	////////////////////////////////// TYF
	if (key.chunkSize > key.digitSize - 11)
	{
	    return "Error";
	}
	////////////////////////////////// TYF


	var a = new Array();
	var sl = s.length;
	
	var i = 0;
	while (i < sl) {
		a[i] = s.charCodeAt(i);
		i++;
	}

	//while (a.length % key.chunkSize != 0) {
	//	a[i++] = 0;
	//}

	var al = a.length;
	var result = "";
	var j, k, block;
	for (i = 0; i < al; i += key.chunkSize) {
		block = new BigInt();
		j = 0;
		
		//for (k = i; k < i + key.chunkSize; ++j) {
		//	block.digits[j] = a[k++];
		//	block.digits[j] += a[k++] << 8;
		//}
		
		////////////////////////////////// TYF
		// Add PKCS#1 v1.5 padding
		// 0x00 || 0x02 || PseudoRandomNonZeroBytes || 0x00 || Message
		// Variable a before padding must be of at most digitSize-11
		// That is for 3 marker bytes plus at least 8 random non-zero bytes
		var x;
		var msgLength = (i+key.chunkSize)>al ? al%key.chunkSize : key.chunkSize;
		
		// Variable b with 0x00 || 0x02 at the highest index.
		var b = new Array();
		for (x=0; x<msgLength; x++)
		{
		    b[x] = a[i+msgLength-1-x];
		}
		b[msgLength] = 0; // marker
		var paddedSize = Math.max(8, key.digitSize - 3 - msgLength);
	
		for (x=0; x<paddedSize; x++) {
		    b[msgLength+1+x] = Math.floor(Math.random()*254) + 1; // [1,255]
		}
		// It can be asserted that msgLength+paddedSize == key.digitSize-3
		b[key.digitSize-2] = 2; // marker
		b[key.digitSize-1] = 0; // marker
		
		for (k = 0; k < key.digitSize; ++j) 
		{
		    block.digits[j] = b[k++];
		    block.digits[j] += b[k++] << 8;
		}
		////////////////////////////////// TYF

		var crypt = key.barrett.powMod(block, key.e);
		var text = key.radix == 16 ? biToHex(crypt) : biToString(crypt, key.radix);
		result += text + " ";
	}
	return result.substring(0, result.length - 1); // Remove last space.
}

function decryptedString(key, s)
{
	var blocks = s.split(" ");
	var result = "";
	var i, j, block;
	for (i = 0; i < blocks.length; ++i) {
		var bi;
		if (key.radix == 16) {
			bi = biFromHex(blocks[i]);
		}
		else {
			bi = biFromString(blocks[i], key.radix);
		}
		block = key.barrett.powMod(bi, key.d);
		for (j = 0; j <= biHighIndex(block); ++j) {
			result += String.fromCharCode(block.digits[j] & 255,
			                              block.digits[j] >> 8);
		}
	}
	// Remove trailing null, if any.
	if (result.charCodeAt(result.length - 1) == 0) {
		result = result.substring(0, result.length - 1);
	}
	return result;
}
