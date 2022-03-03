package com.cdg.krispay.repo;

import com.cdg.krispay.domain.KrisPayTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnLogRepo extends JpaRepository<KrisPayTransaction, Long> {

    KrisPayTransaction findByIdempotencyKey(String idempotentKey);

}
