package client.com.insipidknight.api;



import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("internal/location")
public interface LocationClient {
    @DeleteExchange("/driver/{driverId}")
    void removeDriver(@PathVariable Long driverId);
}
