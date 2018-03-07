package ymyoo.stock.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ymyoo.stock.AdjustmentType;
import ymyoo.stock.Status;
import ymyoo.stock.dto.StockAdjustment;
import ymyoo.stock.entity.ReservedStock;
import ymyoo.stock.entity.Stock;
import ymyoo.stock.repository.ReservedStockRepository;
import ymyoo.stock.repository.StockRepository;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockRestController {
    private static final Logger log = LoggerFactory.getLogger(StockRestController.class);

    private StockRepository stockRepository;

    private ReservedStockRepository reservedStockRepository;

    @Autowired
    public void setStockRepository(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Autowired
    public void setReservedStockRepository(ReservedStockRepository reservedStockRepository) {
        this.reservedStockRepository = reservedStockRepository;
    }

    @PostMapping
    public ResponseEntity tryStockAdjustment(@RequestBody StockAdjustment stockAdjustment) {
        ReservedStock reservedStock = new ReservedStock(AdjustmentType.valueOf(stockAdjustment.getAdjustmentType()),
                stockAdjustment.getProductId(),
                stockAdjustment.getQty(),
                Status.TRY);

        reservedStockRepository.save(reservedStock);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(reservedStock.getId()).toUri();

        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> confirmStockAdjustment(@PathVariable Long id) {
        ReservedStock reservedStock = reservedStockRepository.findOne(id);

        if(reservedStock.getAdjustmentType() == AdjustmentType.REDUCE) {
            Stock stock = stockRepository.findByProductId(reservedStock.getProductId());
            log.info("Before adjustStock : " + stock.toString());

            stock.reduce(reservedStock.getQty());
            stockRepository.save(stock);

            log.info("After adjustStock : " + stock.toString());
        }

        reservedStock.setStatus(Status.CONFIRM);
        reservedStockRepository.save(reservedStock);

        log.info("ReservedStock : " + reservedStock.toString());

        return new ResponseEntity<>(HttpStatus.OK);
    }
}