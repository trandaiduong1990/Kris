package com.cdg.krispay.repo;

import com.cdg.krispay.domain.KrisPayProcessorTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public interface ProcessorTxnLogRepo extends JpaRepository<KrisPayProcessorTransactionLog, Long> {


}
