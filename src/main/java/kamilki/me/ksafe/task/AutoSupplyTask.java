/*
 * Copyright (C) 2021 Kamil Trysiński
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kamilki.me.ksafe.task;

import kamilki.me.ksafe.data.ConfigData;
import kamilki.me.ksafe.data.PluginData;
import kamilki.me.ksafe.util.InventoryUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Map.Entry;

public class AutoSupplyTask extends BukkitRunnable {

    private final ConfigData configData;
    private final PluginData pluginData;
    
    public AutoSupplyTask(final ConfigData configData, final PluginData pluginData) {
        this.configData = configData;
        this.pluginData = pluginData;
    }

    @Override
    public void run() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("ksafe.bypass.supply")) {
                continue;
            }
            
            boolean hasChanged = false;
            final Map<Material, Integer> userSafe = this.pluginData.userSafes.get(player.getUniqueId());
            
            for (final Entry<Material, Integer> limit : this.configData.itemLimits.entrySet()) {
                final Material material = limit.getKey();
                final int limitValue = limit.getValue();
                
                final int invAmount = InventoryUtil.getInventoryAmount(player, material, this.configData);
                if (invAmount >= limitValue) {
                    continue;
                }
                
                final int safeAmount = userSafe.getOrDefault(material, 0);
                if (safeAmount == 0) {
                    continue;
                }
                
                final int toSupply = Math.min(safeAmount, limitValue - invAmount);
                final int notAdded = InventoryUtil.addToInventory(player, material, toSupply, this.configData);
                
                final int newSafeAmount = userSafe.getOrDefault(material, 0) - toSupply + notAdded;
                userSafe.put(material, newSafeAmount);
                
                this.pluginData.changedUsers.add(player.getUniqueId());
                
                String message = this.configData.itemsSuppliedMsg;
                
                message = StringUtils.replace(message, "{ITEM}", this.configData.itemNames.get(material));
                message = StringUtils.replace(message, "{ADDED}", Integer.toString(toSupply - notAdded));
                message = StringUtils.replace(message, "{UNDER}", Integer.toString(toSupply));
                message = StringUtils.replace(message, "{LIMIT}", Integer.toString(limitValue));
                message = StringUtils.replace(message, "{OLD-AMOUNT}", Integer.toString(invAmount));
                message = StringUtils.replace(message, "{NEW-AMOUNT}", Integer.toString(invAmount + toSupply - notAdded));
                message = StringUtils.replace(message, "{OLD-SAFE}", Integer.toString(safeAmount));
                message = StringUtils.replace(message, "{NEW-SAFE}", Integer.toString(newSafeAmount));
                
                player.sendMessage(message);
                hasChanged = true;
            }
            
            if (hasChanged) {
                final String title = player.getOpenInventory().getTitle();

                if (!title.equals(this.configData.inventoryTitle)) {
                    continue;
                }

                player.closeInventory();
                Bukkit.dispatchCommand(player, "ksafe autoreopen");
            }
        }
    }
    
}
