package czechbol.dynamicchestshop.staticshop;

import czechbol.dynamicchestshop.DynamicChestShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Chest;

import static czechbol.dynamicchestshop.staticshop.ChestShop.*;

public class ChestShopHandler implements Listener {

    /**
     * todo: Locale
     * todo: check if chest is there and invulnerable it
     * */
    @EventHandler
    public void OnSignPlace(SignChangeEvent e) {
        var block = e.getBlock();
        var player = e.getPlayer();

        if (!e.getLine(NAME_LINE).equalsIgnoreCase("[ChestShop]")) return;
        if(!block.getWorld().getBlockAt(block.getLocation()
                .subtract(0,1,0)).getType().equals(Material.CHEST)) {
            player.sendMessage("ChestShop: In order to create chestshop, you have to place chest first");
            return;
        }
        if(block.getType() == Material.CHEST){

        }

        e.setLine(NAME_LINE, String.format("[%s]", player.getName()));

        try {
            var quantity = Integer.parseInt(e.getLine(QUANTITY_LINE).strip());
            e.setLine(QUANTITY_LINE, String.format("%d", quantity));
        } catch (Exception exp) {
            player.sendMessage("ChestShop: Required quantity must be Integer");
            e.setCancelled(true);
            return;
        }

        try {
            e.setLine(PRICES_LINE, ChestShop.formatPrices(e.getLine(PRICES_LINE)));
        } catch (Exception exp) {
            player.sendMessage("ChestShop: Could not be created");
            e.setCancelled(true);
            return;
        }

        var material = Material.matchMaterial(e.getLine(MATERIAL_LINE));

        if (material != null) {
            e.setLine(MATERIAL_LINE, material.toString());
        } else {
            player.sendMessage("ChestShop: Invalid item");
        }

        //todo: set invulnerability

        player.sendMessage("ChestShop: Shop was created");
    }

    /**
     * todo: No buy price osetrenie
     * */
    @EventHandler
    public void OnSignInteract(PlayerInteractEvent e) {
        var action = e.getAction();

        if(!action.equals(Action.LEFT_CLICK_BLOCK)
                && !action.equals(Action.RIGHT_CLICK_BLOCK)) return;

        var block = e.getClickedBlock();

        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            //todo: check if chestshop

            if(!sign.getLine(NAME_LINE).equals("[ChestShop]")) return;

            var quantity = Integer.parseInt(sign.getLine(QUANTITY_LINE));
            var material = Material.getMaterial(sign.getLine(MATERIAL_LINE));
            var player = e.getPlayer();

            switch (action) {
                case LEFT_CLICK_BLOCK -> {
                    var price = ChestShop.getBuyPrice(sign.getLine(PRICES_LINE));
                    if(price == -1) return;

                    if(DynamicChestShop.getEcon().getBalance(player) >= price) {
                        if(player.getInventory().firstEmpty() == -1) {
                            ItemStack[] content = player.getInventory().getContents();
                            var freeSpace = 0;
                            for (ItemStack is : content) {
                                if(is == null) continue;
                                if (is.getType().equals(material)
                                        && is.getMaxStackSize() - is.getAmount() >= 0) {
                                    freeSpace += is.getMaxStackSize() - is.getAmount();
                                }
                            }
                            if (freeSpace < quantity) {
                                player.sendMessage("AdminShop: Your inventory is full");
                                return;
                            }
                        }
                        DynamicChestShop.getEcon().withdrawPlayer(player, price);
                        player.getInventory().addItem(new ItemStack(material, quantity));
                        //TODO: Change global price on buy accordingly
                    } else {
                        player.sendMessage("ChestShop: You do not have enough money");
                    }
                }

                case RIGHT_CLICK_BLOCK -> {
                    var price = ChestShop.getSellPrice(sign.getLine(PRICES_LINE));
                    if(price == -1) return;

                    PlayerInventory playerInventory = player.getInventory();
                    ItemStack itemStack = new ItemStack(material, quantity);
                    if(playerInventory.containsAtLeast(itemStack, quantity)){
                        DynamicChestShop.getEcon().depositPlayer(player, price);
                        ItemStack[] content = playerInventory.getContents();
                        for(ItemStack is : content){
                            if(is != null && is.getType().equals(material)) {
                                var amountOfItems = is.getAmount();
                                if(amountOfItems >= quantity){
                                    is.setAmount(amountOfItems-quantity);
                                    break;
                                } else {
                                    is.setAmount(0);
                                    quantity = quantity-amountOfItems;
                                }
                            }
                        }
                        //TODO: Change global price on sell accordingly
                    } else {
                        player.sendMessage("ChestShop: You do not have enough items to sell");
                    }
                }

                default -> throw new IllegalStateException("Unexpected value: " + action);
            }
        }
    }
}
