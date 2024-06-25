package Agents;

public enum AgentType {

     dl, erp_l, markets, mk, sc;

    @Override
    public String toString() {
        return switch (this) {
            case sc -> "sc";
            case dl -> "dl";
            case mk -> "mk";
            case erp_l -> "erp_l";
            case markets -> "markets";
        };
    }
}
