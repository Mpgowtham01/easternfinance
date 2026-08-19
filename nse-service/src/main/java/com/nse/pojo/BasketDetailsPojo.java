package com.nse.pojo;

import com.nse.dto.mf.BasketDetailsDto;
import lombok.Data;

import java.util.List;

@Data
public class BasketDetailsPojo
{
    public String basket_name;
    public List<BasketDetailsDto> list;
}
