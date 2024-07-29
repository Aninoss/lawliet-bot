package dashboard.components

import dashboard.ActionResult
import dashboard.component.DashboardButton
import dashboard.component.DashboardButton.Style
import dashboard.container.HorizontalContainer
import dashboard.container.HorizontalPusher
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.max

class PageField(page: Int, size: Int, entriesPerPage: Int, pageSwitchConsumer: (Int) -> Unit): HorizontalContainer() {

    init {
        val container = HorizontalContainer()
        val pageSize = (size - 1) / entriesPerPage + 1
        val pageNumbers = getVisiblePageNumbers(page, pageSize)

        pageNumbers.forEach { i ->
            val button: DashboardButton
            if (i != -1) {
                button = DashboardButton((i + 1).toString()) {
                    pageSwitchConsumer(i)
                    ActionResult()
                        .withRedraw()
                }
                button.isEnabled = page != i
            } else {
                button = DashboardButton("…")
                button.isEnabled = false
            }
            button.style = Style.TERTIARY
            container.add(button)
        }

        container.putCssProperty("margin-left", "0")
        add(HorizontalPusher(), container, HorizontalPusher())
    }

    private fun getVisiblePageNumbers(page: Int, pageSize: Int): List<Int> {
        val pages: MutableList<Int>
        if (pageSize <= 7) {
            pages = IntStream.range(0, pageSize).boxed().collect(Collectors.toList<Int>())
        } else {
            pages = ArrayList()

            if (page <= 3) {
                pages.addAll(IntStream.rangeClosed(0, page).boxed().collect(Collectors.toList()))
            } else {
                pages.addAll(java.util.List.of(0, -1))
                val minPage = kotlin.math.min((page - 1).toDouble(), (pageSize - 5).toDouble()).toInt()
                for (i in minPage..page) {
                    pages.add(i)
                }
            }

            if (page >= pageSize - 4) {
                pages.addAll(IntStream.range(page + 1, pageSize).boxed().collect(Collectors.toList<Int>()))
            } else {
                val maxPage = max((page + 1).toDouble(), 4.0).toInt()
                for (i in page + 1..maxPage) {
                    pages.add(i)
                }
                pages.addAll(java.util.List.of(-1, pageSize - 1))
            }
        }

        return pages
    }

}