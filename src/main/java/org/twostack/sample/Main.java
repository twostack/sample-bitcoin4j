package org.twostack.sample;

import org.twostack.bitcoin4j.Utils;
import org.twostack.bitcoin4j.address.LegacyAddress;
import org.twostack.bitcoin4j.exception.MnemonicException;
import org.twostack.bitcoin4j.transaction.Transaction;

import java.io.IOException;
import java.math.BigInteger;

public class Main {

    public static void main(String[] args) {
        //********
        //WARNING ! : DON'T RE-USE THIS SEED_WORD SEQUENCE IN YOUR PROJECT (or worse, MAINNET) ! YOU WILL LOSE MONEY !
        //********
        String MNEMONIC = "edge eagle blue panda zone tiger emerge trial limit royal average basket";

        try {

            MyWallet wallet = MyWallet.fromSeed(MNEMONIC);
        }catch(IOException ex){
            ex.printStackTrace();
        } catch(MnemonicException ex){
            ex.printStackTrace();
        }

//        LegacyAddress faucetAddress = wallet.getReceivingAddress();  //we "receive" faucet funds here
//        LegacyAddress spendingAddress = wallet.getSpendingAddress();   //our second address

//        System.out.println("Receiving Address :" + faucetAddress); //send Faucet coins here
//        System.out.println("Spending Address : " + spendingAddress); //send received coins here (other key/address we control)

        try {
//            Transaction txn = wallet.sendMoney(spendingAddress, BigInteger.valueOf(10000)); //send 10k satoshis to ourself
//            System.out.println("Transaction ID : " + txn.getTransactionId());
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}
