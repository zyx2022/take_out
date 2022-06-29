package com.zyx.reggie.dto;


import com.zyx.reggie.entity.Setmeal;
import com.zyx.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
