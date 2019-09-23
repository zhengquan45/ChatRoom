import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author zhengquan
 */
@Data
@AllArgsConstructor
public class ServerInfo {
    private String ip;
    private int port;
    private String sn;
}
