#
# Copyright (c) 2019. JetBrains s.r.o.
# Use of this source code is governed by the MIT license that can be found in the LICENSE file.
#
from .core import FeatureSpec, FeatureSpecArray
from .guide import _guide, guides

#
# Plot title
# Scale names: axis labels / legend titles
#
__all__ = ['ggtitle',
           'labs',
           'xlab', 'ylab']


def ggtitle(label, subtitle=None):
    """
    Add title to the plot.

    Parameters
    ----------
    label : str
        The text for the plot title.
    subtitle : str
        The text for the plot subtitle.

    Returns
    -------
    `FeatureSpec`
        Plot title specification.

    Notes
    -----
    Split a long title/subtitle into two lines or more using `\\\\n` as a text separator.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            ggtitle('New Plot Title')

    """
    return labs(title=label, subtitle=subtitle)


def xlab(label):
    """
    Add label to the x axis.

    Parameters
    ----------
    label : str
        The text for the x axis label.

    Returns
    -------
    `FeatureSpec`
        Axis label specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            xlab('x axis label')

    """
    return labs(x=label)


def ylab(label):
    """
    Add label to the y axis.

    Parameters
    ----------
    label : str
        The text for the y axis label.

    Returns
    -------
    `FeatureSpec`
        Axis label specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            ylab('y axis label')

    """
    return labs(y=label)


def labs(title=None, subtitle=None, caption=None, **labels):
    """
    Change plot title, axis labels and legend titles.

    Parameters
    ----------
    title : str
        The text for the plot title.
    subtitle : str
        The text for the plot subtitle.
    caption : str
        The text for the plot caption.
    labels
        Name-value pairs where the name can be:

        - An aesthetic name
        - 'manual' - a key referring to the default custom legend
        - A group name referring to a custom legend where the group is defined via the `layer_key()` function

        The value should be a string, e.g. `color="New Color label"`.

    Returns
    -------
    `FeatureSpec` or `FeatureSpecArray`
        Labels specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5-6
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            labs(title='New plot title', subtitle='The plot subtitle', caption='The plot caption', \\
                 x='New x axis label', y='New y axis label', size='New legend title')

    |

    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 11

        import numpy as np
        from lets_plot import *
        LetsPlot.setup_html()
        n = 10
        np.random.seed(42)
        x = list(range(n))
        y = np.random.uniform(size=n)
        ggplot({'x': x, 'y': y}, aes('x', 'y')) + \\
            geom_point(color='red', manual_key="point") + \\
            geom_line(color='blue', manual_key="line") + \\
            labs(manual='Zones')

    """
    specs = []

    # handle ggtitle
    if title is not None or subtitle is not None:
        specs.append(FeatureSpec('ggtitle', name=None, text=title, subtitle=subtitle))

    # plot caption
    if caption is not None:
        specs.append(FeatureSpec('caption', name=None, text=caption))

    # guides
    for key, label in labels.items():
        specs.append(guides(**{key: _guide(name=None, title=label)}))

    if len(specs) == 1:
        return specs[0]
    return FeatureSpecArray(*specs)
