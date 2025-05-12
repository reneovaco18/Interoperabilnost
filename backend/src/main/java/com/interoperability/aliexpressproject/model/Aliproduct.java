package com.interoperability.aliexpressproject.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@XmlRootElement(name = "aliproduct", namespace = "http://interoperability.com/aliproduct")
@XmlAccessorType(XmlAccessType.FIELD)
public class Aliproduct {

    @XmlElement(namespace = "http://interoperability.com/aliproduct")
    private String id;

    @XmlElement(namespace = "http://interoperability.com/aliproduct", required = true)
    private String title;

    @XmlElement(namespace = "http://interoperability.com/aliproduct", required = true)
    private String imageUrl;

    @XmlElement(namespace = "http://interoperability.com/aliproduct", required = true)
    private BigDecimal price;

    @XmlElement(namespace = "http://interoperability.com/aliproduct")
    private BigDecimal rating;
}
