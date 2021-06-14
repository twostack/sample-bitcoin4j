package org.twostack.sample;

import org.twostack.bitcoin4j.Address;
import org.twostack.bitcoin4j.ECKey;
import org.twostack.bitcoin4j.PrivateKey;
import org.twostack.bitcoin4j.PublicKey;
import org.twostack.bitcoin4j.address.LegacyAddress;
import org.twostack.bitcoin4j.crypto.DeterministicKey;
import org.twostack.bitcoin4j.crypto.HDKeyDerivation;
import org.twostack.bitcoin4j.crypto.HDPath;
import org.twostack.bitcoin4j.crypto.MnemonicCode;
import org.twostack.bitcoin4j.exception.MnemonicException;
import org.twostack.bitcoin4j.params.NetworkAddressType;
import org.twostack.bitcoin4j.params.NetworkType;
import org.twostack.bitcoin4j.transaction.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.twostack.bitcoin4j.Utils.WHITESPACE_SPLITTER;

public class MyWallet {

    DeterministicKey hdMaster;

    private PrivateKey walletPK1;
    private PrivateKey walletPK2;

    Address receivingAddress, spendingAddress;

    private MyWallet(DeterministicKey dk){
        hdMaster = dk;


        //derive a new hardened keypair from our Master Key
        DeterministicKey xpk1 = dk.derive(1);
        DeterministicKey xpk2 = dk.derive(2);

        this.walletPK1 = new PrivateKey(ECKey.fromPrivate(xpk1.getPrivKey()));
        this.walletPK2 = new PrivateKey(ECKey.fromPrivate(xpk2.getPrivKey()));

        receivingAddress = LegacyAddress.fromKey(NetworkAddressType.TEST_PKH, walletPK1.getPublicKey());
        spendingAddress = LegacyAddress.fromKey(NetworkAddressType.TEST_PKH, walletPK2.getPublicKey());
    }



//    String createWalletTxn(Address address, String prevTxId, Integer outputIndex, BigInteger amount ){
//        TransactionBuilder txBuilder = new TransactionBuilder();
//
//        UnlockingScriptBuilder unlockBuilder= new P2PKHUnlockBuilder(walletPK1.getPublicKey());
//        txBuilder.spendFromOutput(prevTxId, outputIndex, amount, Transaction.NLOCKTIME_MAX_VALUE, unlockBuilder);
//
//        var receiveLockBuilder = P2PKHLockBuilder(address);
//        var changeLockBuilder = P2PKHLockBuilder(_receivingAddress);
//        transaction
//                .spendTo(address, amount, scriptBuilder: receiveLlockBuilder)
//            .sendChangeTo(_receivingAddress, scriptBuilder: changeLockBuilder) // spend change to myself
//            .withFeePerKb(1000);
//
//        transaction.signInput(0, this._walletPrivKey, sighashType: SighashType.SIGHASH_ALL | SighashType.SIGHASH_FORKID);
//
//        return transaction.serialize();
//
//    }

    public static MyWallet fromSeed(String seedPhrase) throws IOException, MnemonicException {
        List<String> words = WHITESPACE_SPLITTER.splitToList(seedPhrase);
        MnemonicCode mc = new MnemonicCode();
        mc.check(words);
        byte[] seedBytes = MnemonicCode.toSeed(words, "");

        DeterministicKey dk = HDKeyDerivation.createMasterPrivateKey(seedBytes);
        return new MyWallet(dk);
    }


//        When sending money we need to
//        1) Get a list of UTXOs we can spend from
//        2) Generate a list of UTXOs that has total amount > sending amount
     public void sendMoney(Address address, BigInteger amount) throws Exception {

         BitIndex bi = new BitIndex(NetworkType.TEST);

         //query the BitIndex API for UTXOs matching our testnet faucet receiving address
         List<TransactionOutpoint> outpoints = bi.getOutpoints(this.receivingAddress);

         TransactionBuilder txBuilder = new TransactionBuilder();
         UnlockingScriptBuilder unlockBuilder = new P2PKHUnlockBuilder(walletPK1.getPublicKey());

         BigInteger sendingAmount = BigInteger.ZERO;

         List<TransactionOutpoint> outpointsToSign = new ArrayList<>();

         //assemble a minimum number of UTXOs to satisfy the amount we are sending
         for (TransactionOutpoint outpoint : outpoints) {
             txBuilder.spendFromOutput(
                     outpoint.getTransactionId(),
                     outpoint.getOutputIndex(),
                     outpoint.getSatoshis(),
                     Transaction.NLOCKTIME_MAX_VALUE,
                     unlockBuilder);

             outpointsToSign.add(outpoint);

             if (sendingAmount.compareTo(amount) == -1) break;

             sendingAmount = sendingAmount.add(outpoint.getSatoshis());
         }

         LockingScriptBuilder receiveLockBuilder = new P2PKHLockBuilder(address);
         LockingScriptBuilder changeLockBuilder = new P2PKHLockBuilder(spendingAddress);

         //FIXME: Change this external API to use Coin values ??
         Transaction unsignedTxn = txBuilder.spendTo(receiveLockBuilder, BigInteger.valueOf(50000000L)) //spend half of a bitcoin (we should have 1 in the UTXO)
             .sendChangeTo(spendingAddress, changeLockBuilder) // spend change to myself
             .withFeePerKb(1024)
             .build(false);

         for (int i = 0; i < outpointsToSign.size(); i++) {

             TransactionOutpoint outpoint = outpointsToSign.get(i);
             TransactionOutput output = new TransactionOutput(outpoint.getSatoshis(), outpoint.getLockingScript());

             //sign all the outputs we are spending
             new TransactionSigner().sign(unsignedTxn, output, i, walletPK1, SigHashType.ALL.value | SigHashType.FORKID.value);

         }
         //we still don't have enough money.
         if (sendingAmount.compareTo(amount) == -1) {
             throw new Exception("You don't have enough money :(");
         }



     }

}
