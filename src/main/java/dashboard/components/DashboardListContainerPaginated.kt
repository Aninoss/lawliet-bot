package dashboard.components

import dashboard.DashboardComponent
import dashboard.container.DashboardListContainer
import dashboard.container.VerticalContainer
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.math.min

class DashboardListContainerPaginated(items: Collection<DashboardComponent>, suggestedPage: Int, pageSwitchConsumer: (Int) -> Unit) : VerticalContainer() {

    private val ENTRIES_PER_PAGE = 25

    init {
        val maxPage = max(0, items.size - 1) / ENTRIES_PER_PAGE
        val page = min(suggestedPage, maxPage)
        if (suggestedPage > maxPage) {
            pageSwitchConsumer.invoke(maxPage)
        }

        val container = DashboardListContainer()
        container.add(items.stream().skip(page * ENTRIES_PER_PAGE.toLong()).limit(ENTRIES_PER_PAGE.toLong()).collect(Collectors.toList()))
        add(container)

        if (items.size > ENTRIES_PER_PAGE) {
            val pageField = PageField(page, items.size, ENTRIES_PER_PAGE, pageSwitchConsumer)
            pageField.putCssProperties("margin-top", "0.75em")
            add(pageField)
        }
    }

}
