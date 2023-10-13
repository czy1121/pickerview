# PickerView

使用 RecyclerView 实现的滚轮控件，这个可以显示3个以上元素(系统自带的 NumberPicker 写死了只能显示3个)。

- pickerview - 基础滚轮控件
- pickerview-datetime - 日期选择器(DatePickerView) 和 时间选择器(TimePickerView)
- pickerview-chinesedate - 农历日期选择器

## Gradle

``` groovy
repositories {
    maven { url "https://gitee.com/ezy/repo/raw/cosmo/"}
}
dependencies {
    implementation "me.reezy.cosmo:pickerview:0.8.0"
    implementation "me.reezy.cosmo:pickerview-datetime:0.8.0"
    implementation "me.reezy.cosmo:pickerview-chinesedate:0.8.0"
}
```


## LICENSE

The Component is open-sourced software licensed under the [Apache license](LICENSE).