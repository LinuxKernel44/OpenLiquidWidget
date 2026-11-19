-dontwarn org.jetbrains.annotations.**
# Widget providers are instantiated reflectively by WidgetKind / GlassWidgetProvider.updateAll.
-keep class io.github.linuxkernel44.openliquidwidget.widget.** extends android.appwidget.AppWidgetProvider { <init>(); }
