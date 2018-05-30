package ymyoo.stock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ymyoo.stock.dto.ParticipantLink;
import ymyoo.stock.dto.StockAdjustment;
import ymyoo.stock.entity.ReservedStock;
import ymyoo.stock.service.StockService;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockRestController {
    private StockService stockService;

    @Autowired
    public void setStockService(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<ParticipantLink> tryStockAdjustment(@RequestBody StockAdjustment stockAdjustment) {
        final ReservedStock reservedStock = stockService.reserveStock(stockAdjustment);

        final ParticipantLink participantLink = buildParticipantLink(reservedStock.getId(), reservedStock.getExpires());

        return new ResponseEntity<>(participantLink, HttpStatus.CREATED);
    }

    private ParticipantLink buildParticipantLink(final Long id, final Date expired) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();

        return new ParticipantLink(location, expired.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }


    @PutMapping("/{id}")
    public ResponseEntity<Void> confirmStockAdjustment(
            @PathVariable Long id,
            @RequestHeader(value="tcc-confirmed-time") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime confirmedTime) {
        try {
            stockService.confirmStock(id, confirmedTime);
        } catch(IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelStockAdjustment(@PathVariable Long id) {
        stockService.cancelStock(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
