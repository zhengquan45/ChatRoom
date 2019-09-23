/**
 * @author Administrator
 */
public interface UDPConstants {

    /**
     * 公用头部
     */
    byte[] HEADER = new byte[]{7, 7, 7, 7, 7, 7, 7, 7};
    /**
     * 服务器端口
     */
    int PORT_SERVER = 30201;
    /**
     * 客户端回复端口
     */
    int PORT_CLIENT_RESPONSE = 30202;
}
