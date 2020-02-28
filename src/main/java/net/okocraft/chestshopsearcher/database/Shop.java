package net.okocraft.chestshopsearcher.database;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Table(name = "shops")
@Entity
public class Shop implements Serializable {

    private static final long serialVersionUID = -8014797515813856571L;

    public Shop() {
    }

    public Shop(String location, String ownerUniqueId, Integer stock, Integer quantity, String item, String buyPrice, String sellPrice) {
        this.location = location;
        this.ownerUniqueId = ownerUniqueId;
        this.stock = stock;
        this.quantity = quantity;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    /** 看板の場所。world,x,y,zというフォーマットで、x,y,zは整数。 */
    @Id
    @NotBlank
    private String location;
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    /** ショップの持ち主のUUID。00000000-0000-0000-0000-000000000000という形式の文字列。 */
    @Column(name = "owner", nullable = false, length = 36)
    @NotBlank
    private String ownerUniqueId;
    public String getOwnerUniqueId() { return ownerUniqueId; }
    public void setOwnerUniqueId(String ownerUniqueId) { this.ownerUniqueId = ownerUniqueId; }

    /** 現在のショップの在庫数。取引するたびに更新する。 */
    @Column(nullable = false)
    @NotNull
    private Integer stock;
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    /** ショップの一度の取引数。取引するたびに更新する。 */
    @Column(nullable = false)
    @NotNull
    @Min(value = 1)
    private Integer quantity;
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    /** ショップのアイテム。チェストショップにおける、アイテム名を採用している。 */
    @Column(nullable = false)
    @NotBlank
    private String item;
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    /** アイテムの販売価格。買取のみの場合は-1を設定する。 */
    @Column(name = "buy_price", nullable = false)
    @NotBlank
    private String buyPrice;
    public String getBuyPrice() { return buyPrice; }
    public void setBuyPrice(String buyPrice) { this.buyPrice = buyPrice; }
    
    /** アイテムの買取価格。販売のみの場合は-1を設定する。 */
    @Column(name = "sell_price", nullable = false)
    @NotBlank
    private String sellPrice;
    public String getSellPrice() { return sellPrice; }
    public void setSellPrice(String sellPrice) { this.sellPrice = sellPrice; }

}