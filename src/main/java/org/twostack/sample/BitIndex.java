package org.twostack.sample;

import org.twostack.bitcoin4j.Address;
import org.twostack.bitcoin4j.params.NetworkType;
import org.twostack.bitcoin4j.transaction.TransactionOutpoint;
import org.twostack.bitcoin4j.transaction.TransactionOutput;

import java.util.List;

public class BitIndex {

    NetworkType networkType;
    public BitIndex(NetworkType networkType){
       this.networkType = networkType;
    }

    public List<TransactionOutput> getUTXOs(Address receivingAddress) {
        return null;
    }

    public List<TransactionOutpoint> getOutpoints(Address receivingAddress) {
        return null;
    }
}
