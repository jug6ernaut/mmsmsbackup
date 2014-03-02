package com.jug6ernaut.sms;

import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by williamwebb on 1/24/14.
 */
public class MenuCreator {

    public static interface MenuOperation{
        public boolean operation(MenuItem item);
    }

    Map<Integer,TopLevelMenu> topLevelMenuList = new HashMap<Integer,TopLevelMenu>();
    private Menu menu;

    public MenuCreator(Menu menu){
        this.menu = menu;
    }

    public TopLevelMenu addTopLevelMenu(int group, String text){
        TopLevelMenu tlm = new TopLevelMenu(group, text, menu);
        tlm.getMenuItem().setIcon(android.R.drawable.ic_menu_info_details);
        tlm.getMenuItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        topLevelMenuList.put(group,tlm);
        return tlm;
    }

    public void addTopLevelOperation(int group, int drawable, MenuOperation operation){
        TopLevelMenu tlm = new TopLevelMenu(group, "", menu);
        tlm.addTopLevelOperation(operation);
        tlm.getMenuItem().setIcon(drawable);
        tlm.getMenuItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        topLevelMenuList.put(group,tlm);
    }

    public static class TopLevelMenu{
        Map<String,MenuOperation> items = new HashMap<String,MenuOperation>();

        android.view.SubMenu subMenu;
        int group;

        public TopLevelMenu(int group, String text, Menu menu){
            this.subMenu = menu.addSubMenu(group, 1, 0, text);
            this.group = group;
        }

        public void addItem(String text,int order, MenuOperation operation){
            subMenu.add(group,0,order,text);
            items.put(key(group,0,order),operation);
        }

        protected boolean handleMenuPress(MenuItem item){
            MenuOperation operation = items.get(key(item));
            if(operation!=null){
                return operation.operation(item);
            } else return false;
        }

        protected void addTopLevelOperation(MenuOperation operation){
            items.put(key(group,1,0),operation);
        }

        protected MenuItem getMenuItem(){
            return subMenu.getItem();
        }
    }

    public boolean handleMenuPress(MenuItem item){
        TopLevelMenu tlm = topLevelMenuList.get(new Integer(item.getGroupId()));
        if(tlm!=null){
            return tlm.handleMenuPress(item);
        } else return false;
    }

    private static String key(MenuItem item){
        return key(item.getGroupId(),item.getItemId(),item.getOrder());
    }

    private static String key(int group, int id, int order){
        return group+"_"+id+"_"+order;
    }
}
