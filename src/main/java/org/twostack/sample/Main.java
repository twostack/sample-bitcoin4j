package org.twostack.sample;

import org.twostack.bitcoin4j.ECKey;
import org.twostack.bitcoin4j.PrivateKey;
import org.twostack.bitcoin4j.Utils;
import org.twostack.bitcoin4j.address.LegacyAddress;
import org.twostack.bitcoin4j.crypto.DumpedPrivateKey;
import org.twostack.bitcoin4j.exception.*;
import org.twostack.bitcoin4j.params.NetworkAddressType;
import org.twostack.bitcoin4j.params.NetworkType;
import org.twostack.bitcoin4j.script.Interpreter;
import org.twostack.bitcoin4j.script.Script;
import org.twostack.bitcoin4j.transaction.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    private String getEncodedPrivateKey(){
        //generate a new elliptic curve Keypair
        ECKey ecKey = new ECKey();

        //Dump our private key so we can serialize it for later recovery.
        //We intend to use it only on the TEST network with 'fake' bitcoins.
        DumpedPrivateKey dumpedKey = ecKey.getPrivateKeyEncoded(NetworkType.TEST);

        //print the text-encoded version of our private key
        return dumpedKey.toBase58();
    }

    ECKey recoverDumpedKey(String encoded){

        //We can recover our private key by reconstructing it from our previous dump
        try {
            DumpedPrivateKey recoveredKey = DumpedPrivateKey.fromBase58(NetworkType.TEST, encoded);
            return recoveredKey.getKey();
        }catch(AddressFormatException ex){
            ex.printStackTrace();
        }

        return null;
    }

    private String getAddress(String encoded){

        ECKey ecKey = recoverDumpedKey(encoded);

        PrivateKey privateKey = new PrivateKey(ecKey);
        LegacyAddress address = LegacyAddress.fromKey(NetworkAddressType.TEST_PKH, privateKey.getPublicKey());

        return address.toBase58();
    }

    private Transaction getTransaction(String txHex){
        return Transaction.fromHex(txHex);
    }

    private void createAndPrintPrivateKeyAndAddress(){

        String encodedKey = getEncodedPrivateKey();
        System.out.println("Our Private Key : " + encodedKey);

        String encodedAddress = getAddress(encodedKey);
        System.out.println("Our Address : " + encodedAddress);

    }


    public static void main(String[] args) throws TransactionException, SigHashException, IOException, SignatureDecodeException {

        Main main = new Main();

        //setup private keys
        PrivateKey alicePrivateKey = new PrivateKey(main.recoverDumpedKey("cRyBgcYD4J5A4RTYdovVQ47Mtz6Fha8t2Q5XK2UDwj6joyNMUobo"));
        PrivateKey bobPrivateKey = new PrivateKey(main.recoverDumpedKey("cPiP1JrMRMn7oA6rku92aXh9u7CLN3oecuGTXj6qKs6bQsQ6ghAj"));

        //setup addresses
        LegacyAddress aliceAddress = LegacyAddress.fromKey(NetworkAddressType.TEST_PKH, alicePrivateKey.getPublicKey());
        LegacyAddress bobAddress = LegacyAddress.fromKey(NetworkAddressType.TEST_PKH, bobPrivateKey.getPublicKey());

        //Alice's raw Faucet funding Transaction. Fetched using curl
        //curl --location --request GET https://api.whatsonchain.com/v1/bsv/test/tx/19737e83a7b05fb050eb7e6acbbc41704fdd95e08b3aca8c38dc88761e838058/hex
        String txHex = "01000000013dd8957a31d47b7df4a256e324c35dc1377815cf6cdbff5663864d0bd2523377010000006a473044022100fb0af69e806863e12ea7a23c49254db064258082ebc903cacff7815305dd654d021f2f4d661e7f94e3e3ed5a56e2951ad9479004369bbeea113a2fb69a1745c06e4121033a2d10f7d3c91b61b3c6d8eca44056ed48c89e3b3c0881e5b5c05d252bd3c0f7ffffffff020e060000000000001976a91446d8ef869272ec048d4450210b2564b75ef4e41388ac50ea0054020000001976a914115a2db5e1da483fb41f332cdff1375bc22f706488ac00000000";

        //Reconstruct the transaction
        Transaction aliceTx = main.getTransaction(txHex);

        //Alice needs an UnlockingScriptBuilder to let her spend her coins
        UnlockingScriptBuilder unlockBuilder = new P2PKHUnlockBuilder(alicePrivateKey.getPublicKey());

        //Alice needs to construct LockingScriptBuilders to ensure that the outputs in the
        //new transaction she creates are spendable by herself (her change output) and Bob (his received funds).
        LockingScriptBuilder receiveLockBuilder = new P2PKHLockBuilder(bobAddress);  //bob can spend from his private key/address
        LockingScriptBuilder changeLockBuilder = new P2PKHLockBuilder(aliceAddress); //alice receives change

        try {

            TransactionBuilder txBuilder = new TransactionBuilder();
            Transaction transactionToSign = txBuilder.spendFromTransaction(aliceTx, 0, Transaction.NLOCKTIME_MAX_VALUE, unlockBuilder)
                    .spendTo(receiveLockBuilder, BigInteger.valueOf(512L)) //spend 512 satoshis to Bob
                    .sendChangeTo(aliceAddress, changeLockBuilder) // spend change to Alice
                    .withFeePerKb(512)  //set the fee to 1 satoshi/byte
                    .build(true);


            //Alice needs to sign over the output in her funding transaction from the Faucet
            TransactionOutput fundingOutput = aliceTx.getOutputs().get(0);

            //sign all the outputs we are spending
            new TransactionSigner().sign(transactionToSign, fundingOutput, 0, alicePrivateKey, SigHashType.ALL.value | SigHashType.FORKID.value);


            Set<Script.VerifyFlag> verifyFlags = new HashSet<>();

            //Let's do a quick sanity check against the Script Interpreter
            //This is a useful example of how to test your own Locking/Unlocking Scripts
            try {
                Interpreter interp = new Interpreter();
                interp.correctlySpends(transactionToSign.getInputs().get(0).getScriptSig(), fundingOutput.getScript(), aliceTx, 0, verifyFlags);
            }catch(Exception ex){
                System.out.println(ex.getMessage());
            }

            System.out.println(Utils.HEX.encode(transactionToSign.serialize()));

        }catch(TransactionException ex){
            System.out.println("Error Occurred: " + ex.getMessage());
        }

    }
}
