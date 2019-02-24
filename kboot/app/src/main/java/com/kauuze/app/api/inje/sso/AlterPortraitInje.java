package com.kauuze.app.api.inje.sso;

import com.kauuze.app.include.annotation.valid.Url;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author kauuze
 * @email 3412879785@qq.com
 * @time 2019-02-24 12:30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class AlterPortraitInje {
    @Url(require = false)
    private String url;
}
