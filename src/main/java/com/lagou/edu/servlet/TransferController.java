package com.lagou.edu.servlet;

import com.lagou.edu.annotation.CustomAutowired;
import com.lagou.edu.annotation.Repository;
import com.lagou.edu.service.TransferService;

@Repository
public class TransferController {

    @CustomAutowired
    private TransferService transferService;

    public void trans() throws Exception {
        String fromCardNo = "6029621011001";
        String toCardNo = "6029621011000";
        int money = 100;
        transferService.transfer(fromCardNo,toCardNo,money);
    }
}
